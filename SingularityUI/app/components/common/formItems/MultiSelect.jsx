import Select from 'react-select';
import React, { PropTypes } from 'react';

// Wrapper for multi-selects in which pressing comma, space, or enter,
// or blurring, causes the option to be autocompleted and added.

const MultiSelect = (props) => {
  const addNewOption = (valueToAdd) => {
    let cleansedValueToAdd = valueToAdd;
    if (props.splits) {
      for (const split of props.splits) {
        cleansedValueToAdd = cleansedValueToAdd.split(split).join('');
      }
    }
    if (!cleansedValueToAdd || props.value.indexOf(cleansedValueToAdd) !== -1) {
      return;
    }
    if (props.options && !props.options.filter(option => option.value === cleansedValueToAdd)) {
      return;
    }
    const newValue = props.value.slice() || [];
    newValue.push(cleansedValueToAdd);
    props.onChange(newValue);
  };
  const checkInputChange = (value) => {
    if (props.splits && props.splits.indexOf(value.slice(-1)) !== -1) {
      addNewOption(value);
    }
  };
  return (
    <Select
      id={ props.id }
      onChange={ props.onChange }
      onInputChange={ value => checkInputChange(value) }
      value={ props.value }
      options={ props.options || [{ label: null, value: null}] }
      onBlurResetsInput={ false }
      multi={ true }
      onBlur={ event => addNewOption(event.target.value) }
      placeholder={ props.placeholder || '' }
      allowCreate={ props.allowCreate || !props.options }
    />
  );
};

MultiSelect.propTypes = {
  splits: PropTypes.arrayOf(PropTypes.string),
  value: PropTypes.arrayOf(PropTypes.shape({
    label: PropTypes.string,
    value: PropTypes.string
  })).isRequired,
  options: PropTypes.arrayOf(PropTypes.shape({
    label: PropTypes.string,
    value: PropTypes.string
  })),
  onChange: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  allowCreate: PropTypes.bool,
  id: PropTypes.string.isRequired
};

export default MultiSelect;
