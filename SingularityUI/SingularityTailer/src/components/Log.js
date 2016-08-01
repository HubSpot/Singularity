import React, { PropTypes } from 'react';

import LogLines from './LogLines';

import sandboxTailer from './sandboxTailer';

const Log = ({type, file}) => {
  switch (type) {
    case 'sandbox':
      debugger;
      return sandboxTailer(
        file.taskId,
        file.filename
      )(LogLines);
    default:
      return (
        <div>Unknown tailer type {type}</div>
      );
  }
};

Log.propTypes = {
  type: PropTypes.oneOf([
    'sandbox'
  ]).isRequired,
  file: PropTypes.object.isRequired
};

export default Log;
