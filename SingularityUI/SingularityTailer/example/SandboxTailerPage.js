import React, { PropTypes } from 'react';
import { SandboxTailer } from '../src/components';

const SandboxTailerPage = ({params, location: {query}}) => {
  const taskId = params.taskId;
  const path = params.splat;

  let goToOffset;
  if (query && query.goToOffset) {
    goToOffset = parseInt(query.goToOffset, 10);
  }

  return (
    <SandboxTailer
      tailerId={`${taskId}/${path}`}
      taskId={taskId}
      path={path}
      goToOffset={goToOffset}
    />
  );
};

SandboxTailerPage.propTypes = {
  params: PropTypes.object.isRequired
};

export default SandboxTailerPage;
