import React, {PropTypes} from 'react';
import Select from 'react-select';
import classNames from 'classnames';

const SelectFormGroup = (props) => (
  <div className={classNames('form-group', {required: props.required})}>
    <label htmlFor={props.id}>{props.label}</label>
    <Select
      id={props.id}
      className={props.id}
      options={props.options}
      onChange={props.onChange}
      value={props.value}
      clearable={false}
    />
  </div>
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
