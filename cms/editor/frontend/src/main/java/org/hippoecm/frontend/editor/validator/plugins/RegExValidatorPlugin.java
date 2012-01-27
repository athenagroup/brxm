package org.hippoecm.frontend.editor.validator.plugins;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.validator.JcrFieldValidator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.validation.ValidationException;
import org.hippoecm.frontend.validation.Violation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @version $Id$
 */
public class RegExValidatorPlugin extends AbstractValidatorPlugin {
    @SuppressWarnings({"UnusedDeclaration"})
    private static Logger log = LoggerFactory.getLogger(RegExValidatorPlugin.class);

    private final Pattern pattern;

    private final static String PATTERN_KEY = "regex_pattern";

    public RegExValidatorPlugin(IPluginContext context, IPluginConfig config) throws Exception {
        super(context, config);
        if (config.containsKey(PATTERN_KEY)) {
            pattern = Pattern.compile(config.getString(PATTERN_KEY));
        } else {
            throw new Exception("regex_pattern property should be set in the iplugin configuration of: " + config.getName());
        }
    }

    @Override
    public void preValidation(JcrFieldValidator type) throws Exception {
        if (!"String".equals(type.getFieldType().getType())) {
            throw new StoreException("Invalid validation exception; cannot validate non-string field for emptyness");
        }
    }

    @Override
    public Set<Violation> validate(JcrFieldValidator fieldValidator, JcrNodeModel model, IModel childModel) throws ValidationException {
        Set<Violation> violations = new HashSet<Violation>();
        String value = (String) childModel.getObject();
        if (!pattern.matcher(value).find()) {
            violations.add(fieldValidator.newValueViolation(childModel, getTranslation()));
        }
        return violations;
    }


}
