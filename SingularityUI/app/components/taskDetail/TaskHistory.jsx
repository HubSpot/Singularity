import React, { PropTypes } from 'react';
import Utils from '../../utils';
import Section from '../common/Section';
import SimpleTable from '../common/SimpleTable';

function TaskHistory (props) {
  return (
    <Section title="History">
      <SimpleTable
        emptyMessage="This task has no history yet"
        entries={props.task.taskUpdates.concat().reverse()}
        perPage={5}
        headers={['Status', 'Message', 'Time']}
        renderTableRow={(data, index) => {
          return (
            <tr key={index} className={index === 0 ? 'medium-weight' : ''}>
              <td>{Utils.humanizeText(data.taskState)}</td>
              <td>{data.statusMessage ? data.statusMessage : '—'}</td>
              <td>{Utils.timeStampFromNow(data.timestamp)}</td>
            </tr>
          );
        }}
      />
    </Section>
  );
}

TaskHistory.propTypes = {
  task: PropTypes.shape({
    taskUpdates: PropTypes.arrayOf(PropTypes.shape({
      taskState: PropTypes.string,
      statusMessage: PropTypes.string,
      timestamp: PropTypes.number
    })).isRequired
  }).isRequired
};

export default TaskHistory;
