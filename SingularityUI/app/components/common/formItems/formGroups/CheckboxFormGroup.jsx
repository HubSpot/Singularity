import React, {Component, PropTypes} from 'react';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';
import Checkbox from '../Checkbox';

class CheckboxFormGroup extends Component {
  static propTypes = {
    id: PropTypes.string.isRequired,
    label: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    checked: PropTypes.bool,
    disabled: PropTypes.bool,
    hasTooltip: PropTypes.bool,
    tooltipText: PropTypes.string
  }

  render() {
    const checkbox = (
      <label htmlFor={this.props.id}>
        <Checkbox
          id = {this.props.id}
          onChange = {() => this.props.onChange(!this.props.checked)}
          checked = {this.props.checked}
          disabled = {this.props.disabled}
          noFormControlClass = {true}
        />
        {` ${this.props.label}`}
      </label>
    );
    let field;
    if (this.props.hasTooltip) {
      field = (
        <OverlayTrigger
          placement="top"
          overlay={<ToolTip id={`${this.props.id}-tooltip`}>{this.props.tooltipText}</ToolTip>}>
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
  }
}

export default CheckboxFormGroup;
