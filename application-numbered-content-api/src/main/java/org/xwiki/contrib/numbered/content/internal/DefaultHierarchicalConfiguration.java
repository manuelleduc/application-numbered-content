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
package org.xwiki.contrib.numbered.content.internal;

import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.numbered.content.HierarchicalConfiguration;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link HierarchicalConfiguration} where the hierarchy is climbed using {@link
 * EntityReference#getParent()}.
 *
 * @version $Id$
 * @since 1.2
 */
@Component
@Singleton
public class DefaultHierarchicalConfiguration implements HierarchicalConfiguration
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public boolean resolve(XWikiDocument document, Function<XWikiDocument, Optional<Boolean>> predicate)
        throws Exception
    {
        if (document == null) {
            return false;
        }
        if (predicate.apply(document).orElse(false)) {
            return true;
        }
        return internalResolve(document.getDocumentReference().getParent(), predicate);
    }

    @Override
    public boolean resolve(EntityReference entityReference, Function<XWikiDocument, Optional<Boolean>> predicate)
        throws Exception
    {
        return internalResolve(entityReference, predicate);
    }

    private boolean internalResolve(EntityReference entityReference,
        Function<XWikiDocument, Optional<Boolean>> predicate)
        throws Exception
    {
        if (entityReference != null) {
            EntityReference currentReference = entityReference;
            do {
                XWikiDocument actualDoc =
                    (XWikiDocument) this.documentAccessBridge.getDocumentInstance(currentReference);
                Optional<Boolean> apply = predicate.apply(actualDoc);
                if (apply.isPresent()) {
                    return apply.get();
                }
                currentReference = currentReference.getParent();
            } while (currentReference != null);
        }
        return false;
    }
}
