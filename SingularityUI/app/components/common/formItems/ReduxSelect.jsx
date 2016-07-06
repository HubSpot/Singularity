import React from 'react';
import Select from 'react-select';

// Wrapper for react-select for use with redux form. Needs to override onBlur of react-select
// More info: https://github.com/erikras/redux-form/issues/82
export default (props) => {
  return (
    <Select
      {...props}
      onBlur={_.noop}
    />
  );
};
