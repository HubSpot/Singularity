import React, {PropTypes} from 'react';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import Checkbox from '../Checkbox';

const CheckboxFormGroup = (props) => {
  const checkbox = (
    <label htmlFor={props.id}>
      <Checkbox
        id = {props.id}
        onChange = {() => props.onChange(!props.checked)}
        checked = {props.checked}
        disabled = {props.disabled}
        noFormControlClass = {true}
      />
      {` ${props.label}`}
    </label>
  );
  let field;
  if (props.hasTooltip) {
    field = (
      <OverlayTrigger
        placement="top"
        overlay={<ToolTip id={`${props.id}-tooltip`}>{props.tooltipText}</ToolTip>}>
        {checkbox}
      </OverlayTrigger>
    );
  } else {
    field = checkbox;
  }
  return (
    <div className="form-group">
      {field}
    </div>
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
