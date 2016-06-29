import React, {Component, PropTypes} from 'react';
import MultiInput from '../MultiInput';
import classNames from 'classnames';

class MultiInputFormGroup extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    placeholder: PropTypes.string,
    value: PropTypes.arrayOf(PropTypes.string),
    required: PropTypes.bool
  }

  render() {
    return (
      <div className={classNames('form-group', {required: this.props.required})}>
        <label htmlFor={this.props.id}>{this.props.label}</label>
        <MultiInput
          id={this.props.id}
          className={this.props.id}
          value={this.props.value || []}
          onChange={this.props.onChange}
          required={this.props.required}
          placeholder={this.props.placeholder}
        />
      </div>
    );
  }
}

export default MultiInputFormGroup;
