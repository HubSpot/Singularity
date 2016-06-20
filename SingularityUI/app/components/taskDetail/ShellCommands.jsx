import React from 'react';
import { Button } from 'react-bootstrap';

import SimpleTable from '../common/SimpleTable';
import Link from '../common/atomicDisplayItems/Link';
import Utils from '../../utils';

export default class ShellCommands extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      selectedCmd: _.first(config.shellCommands),
      openLog: true
    }
  }

  onOpenLogChange(e) {
    this.setState({
      openLog: e.target.checked
    });
  }

  onCommandChange(e) {
    e.preventDefault();
    this.setState({
      selectedCmd: _.find(config.shellCommands, (c) => c.name == e.target.value)
    });
  }

  render() {
    const options = config.shellCommands.map((c) => {
      return <option key={c.name} value={c.name}>{c.name}</option>;
    });

    const form = this.props.task.isStillRunning && this.props.task.task.taskRequest.deploy.customExecutorCmd.indexOf('singularity-executor') != -1 ? (
      <div className="row">
        <form className="col-md-6">
          <h3>Execute a command</h3>
          <div className="form-group required">
            <label for="cmd">Select command</label>
            <select name="cmd" className="form-control input-large" onChange={this.onCommandChange.bind(this)}>
              {options}
            </select>
            <p className="cmd-description">{this.state.selectedCmd.description}</p>

            <label class="check-label">
              <input type="checkbox" name="openLog" checked={this.state.openLog} onChange={this.onOpenLogChange.bind(this)} /> Redirect to command output upon success
            </label>
          </div>
          <Button bsStyle="success" type="submit">Run</Button>
        </form>
      </div>
    ) : null;

    const history = this.props.task.shellCommandHistory.length ? (
      <div>
        <h3>Command History</h3>
          <SimpleTable
            emptyMessage="No command history"
            entries={this.props.task.shellCommandHistory}
            perPage={5}
            first
            last
            headers={['Timestamp', 'Command', 'User', 'Status', 'Message', '']}
            renderTableRow={(data, index) => {
              console.log(data);
              let updates = _.sortBy(data.shellUpdates, 'timestamp');
              let withFilename = _.find(updates, (u) => u.outputFilename);
              let filename = withFilename ? withFilename.outputFilename : null;
              return (
                <tr key={index}>
                  <td>{Utils.absoluteTimestamp(data.shellRequest.timestamp)}</td>
                  <td><code>data.shellRequest.shellCommand.name</code></td>
                  <td>{data.shellRequest.user}</td>
                  <td>{_.last(updates).updateType}</td>
                  <td>
                    <ul>
                      {updates.map((u) => {
                        return <li key={u.timestamp}>{Utils.absoluteTimestamp(u.timestamp)}: {u.message}</li>
                      })}
                    </ul>
                  </td>
                  <td className="actions-column">
                    {filename ? (
                      <Link prop={{
                          url: `${config.appRoot}/task/${data.shellRequest.taskId.id}/tail/${data.shellRequest.taskId.id}/${filename}`,
                          text: "···",
                          overlayTrigger: true,
                          overlayId: filename,
                          overlayTriggerPlacement: 'left',
                          overlayToolTipContent: 'View output file'
                        }}
                      />
                    ) : null}
                  </td>
                </tr>
              );
            }}
          />
      </div>
    ) : null;

    return (
      <div>
        {form}
        {history}
      </div>
    );
  }
}
