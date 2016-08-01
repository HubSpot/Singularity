import React, {PropTypes} from 'react';
import Select from 'react-select';
import { FormGroup, ControlLabel } from 'react-bootstrap/lib';
import classNames from 'classnames';

const SelectFormGroup = (props) => {
  let selectors;
  if (props.options.length > 5) {
    selectors = (
      <Select
        className={props.id}
        value={props.value || props.defaultValue}
        clearable={props.clearable || false}
        {...props}
      />
    );
  } else {
    selectors = (
      <div id="type" className="btn-group">
        {props.options.map((option, key) => (
          <button
            key={key}
            value={option.value}
            className={classNames('btn', 'btn-default', {active: props.value === option.value || (!props.value && props.defaultValue === option.value) })}
            onClick={event => {event.preventDefault(); props.onChange(option);}}
            disabled={props.disabled}
          >
            {option.label}
          </button>
        ))}
      </div>
    );
  }
  return (
    <FormGroup id={props.id} className={props.required && 'required'}>
      <ControlLabel>{props.label}</ControlLabel>
      {selectors}
    </FormGroup>
  );
};

SelectFormGroup.propTypes = {
  id: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  options: PropTypes.arrayOf(PropTypes.shape({
    value: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired
  })).isRequired,
  onChange: PropTypes.func.isRequired,
  defaultValue: PropTypes.string,
  value: PropTypes.string,
  disabled: PropTypes.bool,
  required: PropTypes.bool,
  clearable: PropTypes.bool
};

export default SelectFormGroup;
