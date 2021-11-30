/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.numbered.content.toc;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.numbered.content.toc.internal.TocTreeBuilder;
import org.xwiki.contrib.numbered.headings.NumberingCacheManager;
import org.xwiki.contrib.numbered.headings.internal.NumberedHeadingsService;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.internal.macro.toc.TocBlockFilter;
import org.xwiki.rendering.internal.macro.toc.TreeParameters;
import org.xwiki.rendering.internal.macro.toc.TreeParametersBuilder;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.toc.TocMacroParameters;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.reference.link.LinkLabelGenerator;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.rendering.wiki.WikiModelException;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Replace the {@link org.xwiki.rendering.internal.macro.toc.TocMacro} with a new implementation where numbered headings
 * numbers are displayed in the ToC.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Named("toc")
@Singleton
public class TocMacro extends AbstractMacro<TocMacroParameters>
{
    private static final String DESCRIPTION = "Generates a Table Of Contents.";

    @Inject
    private NumberedHeadingsService numberedHeadingsService;

    @Inject
    private Provider<WikiModel> wikiModelProvider;

    private TocTreeBuilder tocTreeBuilder;

    private final ClassBlockMatcher classBlockMatcher = new ClassBlockMatcher(HeaderBlock.class);

    /**
     * A parser that knows how to parse plain text; this is used to transform link labels into plain text.
     */
    @Inject
    @Named("plain/1.0")
    private Parser plainTextParser;

    @Inject
    private NumberingCacheManager cacheManager;

    @Inject
    private Logger logger;

    /**
     * Generate link label.
     */
    @Inject
    private LinkLabelGenerator linkLabelGenerator;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public TocMacro()
    {
        super("Table Of Contents", DESCRIPTION, TocMacroParameters.class);

        // Make sure this macro is executed as one of the last macros to be executed since
        // other macros can generate headers which need to be taken into account by the TOC
        // macro.
        setPriority(2000);
        setDefaultCategory(DEFAULT_CATEGORY_NAVIGATION);
    }

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();
        this.tocTreeBuilder =
            new TocTreeBuilder(new TocBlockFilter(this.plainTextParser, this.linkLabelGenerator), this.cacheManager);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public List<Block> execute(TocMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        boolean isNumbered;
        try {
            isNumbered = this.numberedHeadingsService.isNumberedHeadingsEnabled();
        } catch (Exception e) {
            isNumbered = false;
            this.logger.warn("Cannot check if numbered headings are enabled. Cause: [{}]", getRootCauseMessage(e));
        }

        Block rootBlock = getRootBlockBlock(parameters);

        TreeParametersBuilder builder = new TreeParametersBuilder();
        TreeParameters treeParameters = builder.build(rootBlock, parameters, context);
        return this.tocTreeBuilder.build(treeParameters, isNumbered, () -> getHeaderBlocks(treeParameters));
    }

    private List<HeaderBlock> getHeaderBlocks(TreeParameters parameters)
    {
        return parameters.rootBlock.getBlocks(this.classBlockMatcher, Block.Axes.DESCENDANT)
            .stream()
            .map(HeaderBlock.class::cast)
            .filter(h -> {
                // TODO: for now, excludes all blocks coming from internal Macro, a better way to fiter things 
                // out exists probably...
                Block parent = h.getParent();
                while (parent != null) {
                    if (parent instanceof MacroMarkerBlock && parent != parameters.rootBlock) {
                        return false;
                    }
                    parent = parent.getParent();
                }
                return true;
            })
            .filter(h -> h.getLevel().getAsInt() <= parameters.depth)
            .collect(Collectors.toList());
    }

    private Block getRootBlockBlock(TocMacroParameters parameters) throws MacroExecutionException
    {
        Block rootBlock = null;
        WikiModel wikiModel = this.wikiModelProvider.get();
        if (parameters.getReference() != null) {
            if (wikiModel != null) {
                // Remote TOC always has a PAGE scope since a LOCAL scope would have no meaning
                parameters.setScope(TocMacroParameters.Scope.PAGE);
                // Get the referenced source's XDOM
                rootBlock = this.getXDOM(new DocumentResourceReference(parameters.getReference()), wikiModel);
            } else {
                throw new MacroExecutionException(
                    "The \"reference\" parameter can only be used when a WikiModel implementation is available");
            }
        }
        return rootBlock;
    }

    private XDOM getXDOM(DocumentResourceReference reference, WikiModel wikiModel) throws MacroExecutionException
    {
        try {
            return wikiModel.getXDOM(reference);
        } catch (WikiModelException e) {
            throw new MacroExecutionException(String.format("Failed to get XDOM for [%s]", reference), e);
        }
    }
}
