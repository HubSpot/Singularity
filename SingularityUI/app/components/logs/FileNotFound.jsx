import React, {PropTypes} from 'react';

class FileNotFound extends React.Component {
  static propTypes = {
    fileName: PropTypes.string.isRequired,
    noLongerExists: PropTypes.bool
  };

  buildNewRoute() {
    const currentRoute = Backbone.history.getFragment();
    const newRoute = currentRoute.split('/');
    config.runningTaskLogPath.split('/').map(() => newRoute.pop());
    newRoute.push(config.finishedTaskLogPath);
    return `${ config.appRoot }/${ newRoute.join('/') }`;
  }

  render() {
    return (
      <div className="lines-wrapper">
        <div className="empty-table-message">
          <p>
            {_.last(this.props.fileName.split('/'))} {this.props.noLongerExists ? 'no longer exists ' : 'does not exist '}{this.props.fileName && this.props.fileName.indexOf('$TASK_ID') !== -1 ? " in this task's directory" : ' for this task'}.
          </p>
          {this.props.fileName.indexOf(config.runningTaskLogPath) !== -1 && <p>
            It may have been moved to <a href={this.buildNewRoute()}>tail_of_finished_service.log</a>.
          </p>}
        </div>
      </div>
    );
  }
}

export default FileNotFound;

