import React, { Component, PropTypes } from 'react';
import { Link } from 'react-router';
import { Button, Glyphicon } from 'react-bootstrap';
import OverlayTrigger from 'react-bootstrap/lib/OverlayTrigger';
import ToolTip from 'react-bootstrap/lib/Tooltip';

import ShellCommandLauncher from './ShellCommandLauncher';

import SimpleTable from '../common/SimpleTable';
import Utils from '../../utils';

export default class ShellCommands extends Component {

  static propTypes = {
    isStillRunning: PropTypes.bool,
    shellCommandHistory: PropTypes.arrayOf(PropTypes.shape({
      shellUpdates: PropTypes.arrayOf(PropTypes.shape({
        timestamp: PropTypes.number,
        outputFilename: PropTypes.string,
        updateType: PropTypes.string
      })),
      shellRequest: PropTypes.shape({
        user: PropTypes.string,
        timestamp: PropTypes.number,
        shellCommand: PropTypes.shape({
          name: PropTypes.string
        }).isRequired,
        taskId: PropTypes.shape({
          id: PropTypes.string
        }).isRequired
      }).isRequired
    })),
    customExecutorCmd: PropTypes.string,
    shellCommandResponse: PropTypes.shape({
      timestamp: PropTypes.number
    }),
    runShellCommand: PropTypes.func.isRequired,
    updateTask: PropTypes.func.isRequired,
    taskFiles: PropTypes.object,
    updateFiles: PropTypes.func.isRequired
  }

  constructor(props) {
    super(props);
    this.state = {
      selectedCmd: _.first(config.shellCommands),
      openLog: true,
      responseText: null,
      showLauncher: false,
      submitDisabled: config.shellCommands.length === 0
    };
  }

  componentWillUnmount() {
    clearTimeout(this.timeout);
  }

  onOpenLogChange(event) {
    this.setState({
      openLog: event.target.checked
    });
  }

  onCommandChange(event) {
    event.preventDefault();
    this.setState({
      selectedCmd: _.find(config.shellCommands, (shellCommand) => shellCommand.name === event.target.value)
    });
  }

  handleRun(event) {
    event.preventDefault();
    this.setState({
      submitDisabled: true
    });
    this.props.runShellCommand(this.state.selectedCmd.name).then(() => {
      this.setState({
        responseText: 'Command sent!',
        showLauncher: this.state.openLog,
        submitDisabled: false
      });
      this.timeout = setTimeout(() => this.setState({responseText: null}), 5000);
    });
  }

  render() {
    const options = config.shellCommands.map((shellCommand) => {
      return <option key={shellCommand.name} value={shellCommand.name}>{shellCommand.name}</option>;
    });

    const form = this.props.isStillRunning &&
    this.props.customExecutorCmd &&
    this.props.customExecutorCmd.indexOf('singularity-executor') !== -1 && (
      <div className="row">
        <form className="col-md-6">
          <h3>Execute a command</h3>
          <div className="form-group required">
            <label htmlFor="cmd">Select command</label>
            <select name="cmd" className="form-control input-large" onChange={(event) => this.onCommandChange(event)}>
              {options}
            </select>
            <p className="cmd-description">{this.state.selectedCmd && this.state.selectedCmd.description}</p>

            <label className="check-label">
              <input type="checkbox" name="openLog" checked={this.state.openLog} onChange={(event) => this.onOpenLogChange(event)} /> Redirect to command output upon success
            </label>
          </div>
          <Button bsStyle="success" onClick={(event) => this.handleRun(event)} disabled={this.state.submitDisabled}>Run</Button>
          {this.state.responseText && (
            <span className="text-success" style={{marginLeft: '10px'}}>
              <Glyphicon glyph="ok" /> {this.state.responseText}
            </span>
          )}
        </form>
      </div>
    );

    const history = !!this.props.shellCommandHistory.length && (
      <div>
        <h3>Command History</h3>
          <SimpleTable
            emptyMessage="No commands run"
            entries={this.props.shellCommandHistory}
            perPage={5}
            first={true}
            last={true}
            headers={['Timestamp', 'Command', 'User', 'Status', 'Message', '']}
            renderTableRow={(data, index) => {
              const updates = _.sortBy(data.shellUpdates, 'timestamp');
              const withFilename = _.find(updates, (update) => update.outputFilename);
              const filename = withFilename && withFilename.outputFilename;
              return (
                <tr key={index}>
                  <td>{Utils.absoluteTimestamp(data.shellRequest.timestamp)}</td>
                  <td><code>{data.shellRequest.shellCommand.name}</code></td>
                  <td>{data.shellRequest.user}</td>
                  <td>{updates.length && _.last(updates).updateType}</td>
                  <td>
                    <ul>
                      {updates.map((update) => {
                        return <li key={update.timestamp}>{Utils.absoluteTimestamp(update.timestamp)}: {update.message}</li>;
                      })}
                    </ul>
                  </td>
                  <td className="actions-column">
                    {filename && (
                      <OverlayTrigger placement="left" overlay={<ToolTip id={filename}>View output file</ToolTip>}>
                        <Link to={`task/${data.shellRequest.taskId.id}/tail/${data.shellRequest.taskId.id}/${filename}`}>···</Link>
                      </OverlayTrigger>
                    )}
                  </td>
                </tr>
              );
            }}
          />
      </div>
    );

    const launcher = this.state.showLauncher && (
      <ShellCommandLauncher
        commandHistory={this.props.shellCommandHistory}
        close={() => this.setState({showLauncher: false})}
        updateTask={this.props.updateTask}
        updateFiles={this.props.updateFiles}
        taskFiles={this.props.taskFiles}
        shellCommandResponse={this.props.shellCommandResponse}
      />
    );

    return (
      <div>
        {form}
        {history}
        {launcher}
      </div>
    );
  }
}
