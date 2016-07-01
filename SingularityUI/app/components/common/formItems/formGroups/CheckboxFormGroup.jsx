import React, {Component, PropTypes} from 'react';
import Checkbox from '../Checkbox';

class CheckboxFormGroup extends Component {
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
          <Checkbox
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

export default CheckboxFormGroup;
