import React, {PropTypes} from 'react';
import Select from 'react-select';
import { FormGroup, ControlLabel } from 'react-bootstrap/lib';

const SelectFormGroup = (props) => (
  <FormGroup id={props.id} className={props.required && 'required'}>
    <ControlLabel>{props.label}</ControlLabel>
    <Select
      id={props.id}
      className={props.id}
      options={props.options}
      onChange={props.onChange}
      value={props.value}
      clearable={false}
    />
  </FormGroup>
);

SelectFormGroup.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  options: PropTypes.arrayOf(PropTypes.shape({
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired
  })).isRequired,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.string,
  required: PropTypes.bool
};

export default SelectFormGroup;
