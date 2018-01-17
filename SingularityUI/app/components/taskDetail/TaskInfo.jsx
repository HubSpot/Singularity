import React, { PropTypes } from 'react';
import { InfoBox } from '../common/statelessComponents';
import Section from '../common/Section';

function TaskInfo (props) {
  return (
    <Section title="Info">
      <div className="row">
        <ul className="list-unstyled horizontal-description-list">
          <InfoBox name="Task ID" value={props.task.taskId.id} />
          <InfoBox name="Directory" value={props.directory} />
          {props.task.mesosTask.executor && <InfoBox name="Executor GUID" value={props.task.mesosTask.executor.executorId.value} />}
          <InfoBox name="Hostname" value={props.task.offers[0].hostname} />
          {!_.isEmpty(props.ports) && <InfoBox name="Ports" value={props.ports.toString()} />}
          <InfoBox name="Rack ID" value={props.task.rackId} />
          {props.task.taskRequest.deploy.executorData && <InfoBox name="Extra Cmd Line Arguments (for Deploy)" join=" " value={props.task.taskRequest.deploy.executorData.extraCmdLineArgs} />}
          {props.task.taskRequest.pendingTask && props.task.taskRequest.pendingTask.cmdLineArgsList && <InfoBox name="Extra Cmd Line Arguments (for Task)" join=" " value={props.task.taskRequest.pendingTask.cmdLineArgsList} />}
        </ul>
      </div>
    </Section>
  );
}

TaskInfo.propTypes = {
  task: PropTypes.shape({
    taskId: PropTypes.shape({
      id: PropTypes.string
    }).isRequired,
    mesosTask: PropTypes.shape({
      executor: PropTypes.shape({
        executorId: PropTypes.shape({
          value: PropTypes.string
        }).isRequired
      })
    }).isRequired,
    taskRequest: PropTypes.shape({
      deploy: PropTypes.shape({
        executorData: PropTypes.shape({
          extraCmdLineArgs: PropTypes.arrayOf(PropTypes.string)
        })
      }).isRequired,
      pendingTask: PropTypes.shape({
        cmdLineArgsList: PropTypes.arrayOf(PropTypes.string)
      })
    }).isRequired,
    offers: PropTypes.arrayOf(PropTypes.shape({
      hostname: PropTypes.string
    })).isRequired,
    rackId: PropTypes.string
  }).isRequired,
  ports: PropTypes.arrayOf(PropTypes.number),
  directory: PropTypes.string
};

export default TaskInfo;
