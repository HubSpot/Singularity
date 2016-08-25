import React, { PropTypes } from 'react';
import { Link } from 'react-router';
import { SandboxTailer } from '../src/components';

const SandboxTailerPage = ({params, location: {query}}) => {
  const taskId = params.taskId;
  const path = params.splat;

  const myLinkRenderer = (id, offset) => {
    return (
      <span className="default-tailer-link">
        <Link to={`${taskId}/tail/${path}?goToOffset=${offset}`}>
          @
        </Link>
      </span>
    );
  };

  myLinkRenderer.propTypes = {
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
      linkRenderer={myLinkRenderer}
    />
  );
};

SandboxTailerPage.propTypes = {
  params: PropTypes.object.isRequired
};

export default SandboxTailerPage;
