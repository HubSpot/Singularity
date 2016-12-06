import React, { PropTypes } from 'react';
import { SandboxTailer } from '../src/components';

const SandboxTailerPage = ({params, location: {query}}) => {
  const taskId = params.taskId;
  const path = params.splat;

  const myLineLink = ({start}) => (<a href={`/${taskId}/tail/${path}?goToOffset=${start}`}>@</a>);

  myHrefFunc.propTypes = {
    id: PropTypes.string.isRequired,
    offset: PropTypes.number
  };

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
      lineLinkRenderer={myLineLink}
    />
  );
};

SandboxTailerPage.propTypes = {
  params: PropTypes.object.isRequired
};

export default SandboxTailerPage;
