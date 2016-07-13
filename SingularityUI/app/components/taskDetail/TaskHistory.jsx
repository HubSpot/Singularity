import React, { PropTypes } from 'react';

import Utils from '../../utils';

import Section from '../common/Section';
import SimpleTable from '../common/SimpleTable';

const TaskHistory = ({task}) => (
  <Section title="History">
    <SimpleTable
      emptyMessage="This task has no history yet"
      entries={task.taskUpdates.concat().reverse()}
      perPage={5}
      headers={['Status', 'Message', 'Time']}
      renderTableRow={(data, index) => {
        return (
          <tr key={index} className={index === 0 ? 'medium-weight' : ''}>
            <td>{Utils.humanizeText(data.taskState)}</td>
            <td>{data.statusMessage ? data.statusMessage : '—'}</td>
            <td>{Utils.timestampFromNow(data.timestamp)}</td>
          </tr>
        );
      }}
    />
  </Section>
);

TaskHistory.propTypes = {
  task: PropTypes.object.isRequired
};

export default TaskHistory;
