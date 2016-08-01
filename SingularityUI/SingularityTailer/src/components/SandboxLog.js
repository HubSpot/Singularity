import React, { PropTypes } from 'react';

import LogLines from './LogLines';

import sandboxTailer from './sandboxTailer';

const SandboxLog = ({taskId, path}) => {
  return sandboxTailer(
    LogLines,
    taskId,
    path
  );
};

SandboxLog.propTypes = {
  taskId: PropTypes.string.isRequired,
  path: PropTypes.string.isRequired
};

export default SandboxLog;
