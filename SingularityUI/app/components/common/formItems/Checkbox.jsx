import React from 'react';
import FormField from './FormField';

class Checkbox extends React.Component {

  render() {
    return (
      <FormField
        id = {this.props.id}
        noFormControlClass = {this.props.noFormControlClass}
        prop = {{
          updateFn: this.props.onChange,
          inputType: 'checkBox',
          checked: this.props.checked,
          customClass: this.props.className,
          disabled: this.props.disabled
        }}
      />
    );
  }
}

export default Checkbox;
