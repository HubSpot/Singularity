import React, { PropTypes } from 'react';
import Utils from '../../utils';
import Section from '../common/Section';
import SimpleTable from '../common/SimpleTable';
import classNames from 'classnames';

function TaskHistory (props) {
  return (
    <Section title="History">
      <SimpleTable
        emptyMessage="This task has no history yet"
        entries={props.taskUpdates.concat().reverse()}
        perPage={5}
        headers={['Status', 'Message', 'Time']}
        renderTableRow={(data, index) => {
          return (
            <tr key={index} className={classNames({'medium-weight' :index === 0})}>
              <td>{Utils.humanizeText(data.taskState)}</td>
              <td>{data.statusMessage ? data.statusMessage : '—'}</td>
              <td>{Utils.timestampFromNow(data.timestamp)}</td>
            </tr>
          );
        }}
      />
    </Section>
  );
}

TaskHistory.propTypes = {
  taskUpdates: PropTypes.arrayOf(PropTypes.shape({
    taskState: PropTypes.string,
    statusMessage: PropTypes.string,
    timestamp: PropTypes.number
  })).isRequired
};

export default TaskHistory;
