import React, {Component, PropTypes} from 'react';
import CheckBox from '../CheckBox';

class CheckBoxFormGroup extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    checked: PropTypes.bool
  }

  render() {
    return (
      <div className="form-group">
        <label htmlFor={this.props.id}>
          <CheckBox
            id = {this.props.id}
            onChange = {() => this.props.onChange(!this.props.checked)}
            checked = {this.props.checked}
            noFormControlClass = {true}
          />
          {` ${this.props.label}`}
        </label>
      </div>
    );
  }
}

export default CheckBoxFormGroup;
