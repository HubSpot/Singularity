import React from 'react';
import FormField from './FormField';

class CheckBox extends React.Component {

  render() {
    return (
      <FormField
        id = {this.props.id}
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

export default CheckBox;
