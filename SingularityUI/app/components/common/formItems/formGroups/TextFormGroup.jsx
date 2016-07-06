import React, {PropTypes} from 'react';
import FormField from '../FormField';
import classNames from 'classnames';

const TextFormGroup = (props) => {
  const formGroupClassNames = classNames(
    'form-group',
    {
      required: props.required,
      'has-success': props.feedback === 'SUCCESS',
      'has-error': props.feedback === 'ERROR',
      'has-warning': props.feedback === 'WARN',
      'has-feedback': props.feedback
    }
  );
  const iconClassNames = classNames(
    'glyphicon',
    'form-control-feedback',
    {
      'glyphicon-ok': props.feedback === 'SUCCESS',
      'glyphicon-warning-sign': props.feedback === 'WARN',
      'glyphicon-remove': props.feedback === 'ERROR'
    }
  );
  const label = <label htmlFor={props.id}>{props.label}</label>;
  const formField = (
    <FormField
      id={props.id}
      className={props.id}
      prop = {{
        updateFn: props.onChange,
        inputType: 'text',
        value: props.value,
        required: props.required,
        placeholder: props.placeholder
      }}
    />
  );
  const feedback = props.feedback && <span className={iconClassNames} />;
  if (props.inputGroupAddon) {
    return (
      <div className={formGroupClassNames}>
        {label}
        <div className="input-group">
          {formField}
          <div className="input-group-addon">
            {props.inputGroupAddon}
          </div>
        </div>
      </div>
    );
  }
  return (
    <div className={formGroupClassNames}>
      {label}
      {formField}
      {feedback}
    </div>
  );
};

TextFormGroup.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  required: PropTypes.bool,
  feedback: PropTypes.oneOf(['SUCCESS', 'ERROR', 'WARN']),
  inputGroupAddon: PropTypes.oneOfType([PropTypes.element, PropTypes.string])
};

export default TextFormGroup;
