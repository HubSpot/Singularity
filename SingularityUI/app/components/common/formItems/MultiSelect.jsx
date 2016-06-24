import Select from 'react-select';
import React, { PropTypes, Component } from 'react';

// Wrapper for multi-selects in which pressing comma, space, or enter,
// or blurring, causes the option to be autocompleted and added.

class MultiSelect extends Component {

  addNewOption(valueToAdd) {
    let cleansedValueToAdd = valueToAdd;
    if (this.props.splits) {
      this.props.splits.map(split => {
        cleansedValueToAdd = cleansedValueToAdd.split(split).join('');
      })
    }
    if (!cleansedValueToAdd || this.props.value.indexOf(cleansedValueToAdd) !== -1) {
      return;
    }
    if (this.props.options && !this.props.options.filter(option => option.value === cleansedValueToAdd)) {
      return;
    }
    const newValue = this.props.value.slice() || [];
    newValue.push(cleansedValueToAdd);
    this.props.onChange(newValue);
  }

  checkInputChange(value) {
    if (this.props.splits && this.props.splits.indexOf(value.slice(-1)) !== -1) {
      this.addNewOption(value);
    }
  }

  render() {
    return (
      <Select
          id={ this.props.id }
          onChange={ this.props.onChange }
          onInputChange={ value => this.checkInputChange(value) }
          value={ this.props.value }
          options={ this.props.options || [{ label: null, value: null}] }
          onBlurResetsInput={ false }
          multi={ true }
          onBlur={ event => this.addNewOption(event.target.value) }
          placeholder={ this.props.placeholder || "" }
          allowCreate={ this.props.allowCreate || !this.props.options }
        />
    );
  }

}

export default MultiSelect;
