import React, {PropTypes} from 'react';
import { FormGroup, ControlLabel, FormControl, InputGroup } from 'react-bootstrap/lib';

const TextFormGroup = (props) => {
  const formField = (
    <FormControl
      type="text"
      value={props.value || ''}
      placeholder={props.placeholder}
      onChange={(event) => props.onChange(event)}
    />
  );
  const feedback = props.feedback && <FormControl.Feedback />;
  return (
    <FormGroup controlId={props.id} validationState={props.feedback && props.feedback.toLowerCase()} className={props.required && 'required'}>
      <ControlLabel>{props.label}</ControlLabel>
      {props.inputGroupAddon &&
        <InputGroup>
          {formField}
          <InputGroup.Addon>{props.inputGroupAddon}</InputGroup.Addon>
        </InputGroup>
      }
      {!props.inputGroupAddon && formField}
      {!props.inputGroupAddon && feedback}
    </FormGroup>
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
