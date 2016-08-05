import React, { PropTypes } from 'react';
import { Log, sandboxTailer } from '../src/components';

const Tailer = ({params}) => {
  const taskId = params.taskId;
  const path = params.splat;

  const SandboxTailer = sandboxTailer(Log, taskId, path);

  return (
    <SandboxTailer />
  );
};

Tailer.propTypes = {
  params: PropTypes.object.isRequired
};

export default Tailer;
