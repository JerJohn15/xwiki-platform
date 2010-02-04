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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.wizard.Wizard;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.user.client.ui.wizard.WizardStepProvider;

import com.xpn.xwiki.wysiwyg.client.Images;
import com.xpn.xwiki.wysiwyg.client.Strings;
import com.xpn.xwiki.wysiwyg.client.wiki.ResourceName;
import com.xpn.xwiki.wysiwyg.client.wiki.WikiServiceAsync;

/**
 * The link wizard, used to configure link parameters in a {@link com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig}
 * object, in successive steps. This class extends the {@link Wizard} class by encapsulating {@link WizardStepProvider}
 * behavior specific to links.
 * 
 * @version $Id$
 */
public class LinkWizard extends Wizard implements WizardStepProvider
{
    /**
     * Enumeration steps handled by this link wizard.
     */
    public static enum LinkWizardSteps
    {
        /**
         * Steps managed by this wizard.
         */
        WEB_PAGE, EMAIL, WIKI_PAGE, WIKI_PAGE_CREATOR, ATTACHMENT, ATTACHMENT_UPLOAD, WIKI_PAGE_CONFIG
    };

    /**
     * Map with the instantiated steps to return. Will be lazily initialized upon request.
     */
    private Map<LinkWizardSteps, WizardStep> stepsMap = new HashMap<LinkWizardSteps, WizardStep>();

    /**
     * The resource currently edited by this WYSIWYG, used to determine the context in which link creation takes place.
     */
    private final Config config;

    /**
     * The service used to access the wiki.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Builds a {@link LinkWizard} from the passed {@link Config}. The configuration is used to get WYSIWYG editor
     * specific information for this wizard, such as the current page, etc.
     * 
     * @param config the context configuration for this {@link LinkWizard}
     * @param wikiService the service used to access the wiki
     */
    public LinkWizard(Config config, WikiServiceAsync wikiService)
    {
        super(Strings.INSTANCE.link(), Images.INSTANCE.link().createImage());
        this.config = config;
        this.wikiService = wikiService;
        this.setProvider(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardStepProvider#getStep(String)
     */
    public WizardStep getStep(String name)
    {
        LinkWizardSteps requestedStep = parseStepName(name);
        WizardStep step = stepsMap.get(requestedStep);
        if (step == null) {
            switch (requestedStep) {
                case EMAIL:
                    step = new EmailAddressLinkWizardStep();
                    break;
                case WIKI_PAGE:
                    step = new PageSelectorWizardStep(getEditedResource());
                    ((PageSelectorWizardStep) step).setWikiService(wikiService);
                    break;
                case WIKI_PAGE_CREATOR:
                    step = new CreateNewPageWizardStep(getEditedResource());
                    ((CreateNewPageWizardStep) step).setWikiService(wikiService);
                    break;
                case ATTACHMENT:
                    step = dispatchAttachmentSelectorStep();
                    break;
                case ATTACHMENT_UPLOAD:
                    step = new AttachmentUploadWizardStep(getEditedResource());
                    ((AttachmentUploadWizardStep) step).setWikiService(wikiService);
                    break;
                case WIKI_PAGE_CONFIG:
                    step = new LinkConfigWizardStep();
                    break;
                case WEB_PAGE:
                    step = new WebPageLinkWizardStep();
                    break;
                default:
                    // nothing here, leave it null
                    break;
            }
            // if something has been created, add it in the map
            if (step != null) {
                stepsMap.put(requestedStep, step);
            }
        }
        // return the found or newly created step
        return step;
    }

    /**
     * @return the currently edited resource, from the configuration
     */
    private ResourceName getEditedResource()
    {
        return new ResourceName(config.getParameter("wiki"), config.getParameter("space"), config.getParameter("page"),
            null);
    }

    /**
     * @return the wizard step for attachments selector wrt the configuration parameters. If the {@code linkfiles}
     *         parameter with the value {@code currentpage} is not found, then the selector will be enabled for the
     *         whole wiki, otherwise only for the current page.
     */
    private WizardStep dispatchAttachmentSelectorStep()
    {
        String linkFiles = config.getParameter("linkfiles");
        if ("currentpage".equals(linkFiles)) {
            CurrentPageAttachmentSelectorWizardStep step =
                new CurrentPageAttachmentSelectorWizardStep(getEditedResource());
            step.setWikiService(wikiService);
            return step;
        } else {
            AttachmentSelectorWizardStep step = new AttachmentSelectorWizardStep(getEditedResource());
            step.setWikiService(wikiService);
            return step;
        }
    }

    /**
     * Parses the specified step name in a {@link LinkWizardSteps} value.
     * 
     * @param name the name of the step to parse
     * @return the {@link LinkWizardSteps} {@code enum} value corresponding to the passed name, or {@code null} if no
     *         such value exists.
     */
    private LinkWizardSteps parseStepName(String name)
    {
        // let's be careful about this
        LinkWizardSteps requestedStep = null;
        try {
            requestedStep = LinkWizardSteps.valueOf(name);
        } catch (IllegalArgumentException e) {
            // nothing, just leave it null if it cannot be found in the enum
        }
        return requestedStep;
    }
}
