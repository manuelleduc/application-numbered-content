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
package org.xwiki.contrib.numbered.content;

import java.util.Optional;
import java.util.function.Function;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Resolve a configuration by visiting the hierarchy of the document, starting from the document itself. Each level is
 * tested again a predicate indicating the configuration status of the current document. This configuration static can
 * either be unknown (in this case the predicate returns {@link Optional#empty()}), or it can be defined (in this case
 * the predicate returns the configuration status in the {@link Optional}). When the configuration is unknown, the
 * configuration is checked in the parent document, otherwise the configuration of the tested document is checked. If no
 * configuration is found at all, the returned value is {@code false}.
 *
 * @version $Id$
 * @since 1.2
 */
@Role
@Unstable
public interface HierarchicalConfiguration
{
    /**
     * Resolve the document configuration.
     *
     * @param document the document to resolve the configuration for
     * @param predicate the predicate to test the configuration against
     * @return {@code true} if the document has a configuration set to {@code true}, {@code false} if the document has a
     *     configuration set to false, or if no configuration is found
     * @throws Exception in case of error during the resolution of the configuration
     */
    boolean resolve(XWikiDocument document, Function<XWikiDocument, Optional<Boolean>> predicate) throws Exception;

    /**
     * Resolve the document configuration.
     *
     * @param entityReference the entity reference of the document to resolve the configuration for
     * @param predicate the predicate to test the configuration against
     * @return {@code true} if the document has a configuration set to {@code true}, {@code false} if the document has a
     *     configuration set to false, or if no configuration is foundw
     * @throws Exception in case of error during the resolution of the configuration
     */
    boolean resolve(EntityReference entityReference, Function<XWikiDocument, Optional<Boolean>> predicate)
        throws Exception;
}
