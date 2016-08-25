import React, {PropTypes} from 'react';
import { FormGroup, Checkbox } from 'react-bootstrap/lib';

const CheckboxFormGroup = (props) => {
  return (
    <FormGroup controlId={props.id}>
      <Checkbox
        onChange={() => props.onChange(!props.checked)}
        checked={props.checked || false}
        inline={true}
        disabled={props.disabled}
      >
        {props.disabled &&
          <div className="subtle">{props.label} <strong>{props.tooltipText}</strong></div> ||
          <strong>{props.label}</strong>}
      </Checkbox>
    </FormGroup>
  );
};

CheckboxFormGroup.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  checked: PropTypes.bool,
  disabled: PropTypes.bool,
  hasTooltip: PropTypes.bool,
  tooltipText: PropTypes.string
};

export default CheckboxFormGroup;
