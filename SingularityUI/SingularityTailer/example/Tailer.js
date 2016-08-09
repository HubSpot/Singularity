import React, { PropTypes } from 'react';
import { SandboxTailer } from '../src/components';

const Tailer = ({params}) => {
  const taskId = params.taskId;
  const path = params.splat;

  return (
    <SandboxTailer tailerId={`${taskId}/${path}`} taskId={taskId} path={path} />
  );
};

Tailer.propTypes = {
  params: PropTypes.object.isRequired
};

export default Tailer;
