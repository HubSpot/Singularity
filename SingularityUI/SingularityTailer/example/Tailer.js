import React, { PropTypes } from 'react';
import { Log } from '../src/components';

const Tailer = ({params}) => {
  const taskId = params.taskId;
  const path = params.splat;

  return (
    <Log taskId={taskId} path={path} minLines={10} />
  );
};

Tailer.propTypes = {
  params: PropTypes.object.isRequired
};

export default Tailer;
