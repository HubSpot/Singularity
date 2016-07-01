import React, {Component, PropTypes} from 'react';
import Select from 'react-select';
import classNames from 'classnames';

class SelectFormGroup extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    options: PropTypes.arrayOf(PropTypes.shape({
      value: PropTypes.string.isRequired,
      label: PropTypes.string.isRequired
    })).isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.string,
    required: PropTypes.bool
  }

  render() {
    return (
      <div className={classNames('form-group', {required: this.props.required})}>
        <label htmlFor={this.props.id}>{this.props.label}</label>
        <Select
          id={this.props.id}
          className={this.props.id}
          options={this.props.options}
          onChange={this.props.onChange}
          value={this.props.value}
          clearable={false}
        />
      </div>
    );
  }
}

export default SelectFormGroup;
