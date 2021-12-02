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
package org.xwiki.contrib.numberedreferences;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.HeaderBlock;

/**
 * Provides a default behavious for the {@link NumberingService} components where the numbered contents are saved in a
 * {@link NumberingCacheManager}.
 *
 * @version $Id$
 * @since 1.0
 */
public abstract class AbstractNumberingService implements NumberingService
{
    // TODO: duplicated
    private static final String NUMBERED_CONTENT_ROOT_CLASS = "numbered-content-root";

    private static final String START_PARAMETER = "start";

    @Inject
    protected NumberingCacheManager cacheManager;

    @Override
    public List<HeaderBlock> getHeaders(Block rootBlock)
    {
        Optional<List<HeaderBlock>> headersOpt = this.cacheManager.getHeaders(rootBlock);
        List<HeaderBlock> headers;
        if (headersOpt.isPresent()) {
            headers = headersOpt.get();
        } else {
            headers = getHeaderBlocks(rootBlock);
            buildCache(rootBlock, headers);
        }
        return headers;
    }

    @Override
    public Map<HeaderBlock, String> getMap(Block rootBlock)
    {
        Optional<Map<HeaderBlock, String>> headersOpt = this.cacheManager.get(rootBlock);
        Map<HeaderBlock, String> headers;
        if (headersOpt.isPresent()) {
            headers = headersOpt.get();
        } else {
            buildCache(rootBlock, getHeaderBlocks(rootBlock));
            headers = this.cacheManager.get(rootBlock).get();
        }
        return headers;
    }

    /**
     * Return the header blocks that needs to be numbered.
     *
     * @param rootBlock the root block to number
     * @return the list of header blocks to number
     */
    public abstract List<HeaderBlock> getHeaderBlocks(Block rootBlock);

    private void buildCache(Block rootBlock, List<HeaderBlock> headers)
    {
        Map<HeaderBlock, String> rootBlockCache = new HashMap<>();
        this.cacheManager.put(rootBlock, rootBlockCache, headers);

        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(0);
        for (HeaderBlock header : headers) {
            if (!isInGroupBlock(header)) {
                cacheHeader(rootBlockCache, stack, header);
            } else {
                rootBlockCache.put(header, null);
            }
        }
    }

    private boolean isInGroupBlock(HeaderBlock header)
    {
        Block parent = header.getParent();
        while (parent != null) {
            // Stops whenever a content root is found.
            String classes = parent.getParameter("class");
            if (classes != null && classes.contains(NUMBERED_CONTENT_ROOT_CLASS)) {
                return false;
            }
            if (parent instanceof GroupBlock) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    private void cacheHeader(Map<HeaderBlock, String> rootBlockCache, Deque<Integer> stack, HeaderBlock header)
    {
        int level = header.getLevel().getAsInt();
        Integer start = null;
        if (header.getParameter(START_PARAMETER) != null) {
            start = Integer.parseInt(header.getParameter(START_PARAMETER));
        }
        if (level == stack.size()) {
            cacheHeaderSameLevel(rootBlockCache, stack, header, start);
        } else if (level > stack.size()) {
            cacheHeaderLevelIncreses(rootBlockCache, stack, header, level, start);
        } else {
            cacheHeaderLevelDecreases(rootBlockCache, stack, header, level, start);
        }
    }

    private void cacheHeaderLevelDecreases(Map<HeaderBlock, String> rootBlockCache, Deque<Integer> stack,
        HeaderBlock header, int level, Integer start)
    {
        while (stack.size() > level) {
            stack.pop();
        }
        Integer pop = stack.pop();
        if (start != null) {
            stack.push(start);
        } else {
            stack.push(pop + 1);
        }
        rootBlockCache.put(header, serialize(stack));
    }

    private void cacheHeaderLevelIncreses(Map<HeaderBlock, String> rootBlockCache, Deque<Integer> stack,
        HeaderBlock header, int level, Integer start)
    {
        for (int i = stack.size(); i < level; i++) {
            if (i != level - 1) {
                stack.push(0);
            } else {
                if (start != null) {
                    stack.push(start);
                } else {
                    stack.push(1);
                }
            }
        }
        rootBlockCache.put(header, serialize(stack));
    }

    private void cacheHeaderSameLevel(Map<HeaderBlock, String> rootBlockCache, Deque<Integer> stack, HeaderBlock header,
        Integer start)
    {
        Integer pop = stack.pop();
        if (start != null) {
            stack.push(start);
        } else {
            stack.push(pop + 1);
        }
        rootBlockCache.put(header, serialize(stack));
    }

    private String serialize(Deque<Integer> stack)
    {
        List<String> ret = new ArrayList<>();
        Iterator<Integer> integerIterator = stack.descendingIterator();
        while (integerIterator.hasNext()) {
            ret.add(integerIterator.next().toString());
        }
        return StringUtils.join(ret, ".");
    }
}
