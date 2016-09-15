import React, { PropTypes } from 'react';
import { connect } from 'react-redux';

function buildNewRoute(currentPath) {
  const newRoute = currentPath.split('/');
  config.runningTaskLogPath.split('/').map(() => newRoute.pop());
  newRoute.push(config.finishedTaskLogPath);
  const newPath = newRoute.join('/');
  return `${ config.appRoot }/${ newPath.startsWith('/') ? newPath.substring(1) : newPath }`;
}

function FileNotFound (props) {
  return (
    <div className="lines-wrapper">
      <div className="empty-table-message">
        <p>
          {_.last(props.fileName.split('/'))} {props.noLongerExists ? 'no longer exists ' : 'does not exist '}{props.fileName && props.fileName.indexOf('$TASK_ID') !== -1 ? " in this task's directory" : ' for this task'}.
        </p>
        {props.fileName.indexOf(config.runningTaskLogPath) !== -1 && props.finishedLogExists && <p>
          It was moved to <a href={buildNewRoute(props.currentPath)}>tail_of_finished_service.log</a>.
        </p>}
      </div>
    </div>
  );
}

FileNotFound.propTypes = {
  fileName: PropTypes.string.isRequired,
  noLongerExists: PropTypes.bool,
  finishedLogExists: PropTypes.bool,
  currentPath: PropTypes.string.isRequired
};

function mapStateToProps(state) {
  return {
    currentPath: state.routing.locationBeforeTransitions.pathname
  };
}

export default connect(mapStateToProps)(FileNotFound);

