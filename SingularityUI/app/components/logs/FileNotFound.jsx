import React, {PropTypes} from 'react';

const RENAMED_FILES = {
  'service.log': 'tail_of_finished_service.log'
};

class FileNotFound extends React.Component {
  static propTypes = {
    fileName: PropTypes.string.isRequired
  };

  buildNewRoute(fileName) {
    const currentRoute = Backbone.history.getFragment();
    const newRoute = currentRoute.split('/');
    newRoute.pop();
    newRoute.push(RENAMED_FILES[fileName]);
    return `${ config.appRoot }/${ newRoute.join('/') }`;
  }

  render() {
    const fileName = _.last(this.props.fileName.split('/'));
    return (
      <div className="lines-wrapper">
        <div className="empty-table-message">
          <p>
            {fileName} does not exist{this.props.fileName && this.props.fileName.indexOf('$TASK_ID') !== -1 ? " in this task's directory" : ' for this task'}.
          </p>
          {RENAMED_FILES[fileName] && <p>
            It may have been moved to <a href={this.buildNewRoute(fileName)}>tail_of_finished_service.log</a>.
          </p>}
        </div>
      </div>
    );
  }
}

export default FileNotFound;

