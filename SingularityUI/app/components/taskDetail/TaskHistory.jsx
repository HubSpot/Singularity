import React, { PropTypes } from 'react';
import Utils from '../../utils';
import Section from '../common/Section';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import classNames from 'classnames';

function TaskHistory (props) {
  const previousHistories = _.flatten(_.pluck(_.filter(props.taskUpdates, (update) => {return update.previous.length > 0}), 'previous'));

  return (
    <Section title="History">
      <UITable
        emptyTableMessage="This task has no history yet"
        data={_.sortBy(props.taskUpdates.concat(previousHistories), 'timestamp').reverse()}
        keyGetter={(taskUpdate) => taskUpdate.timestamp}
        rowChunkSize={5}
        paginated={false}
        rowClassName={(rowData, index) => classNames({'medium-weight': index === 0})}
      >
        <Column
          label="Status"
          id="status"
          key="status"
          cellData={(taskUpdate) => Utils.humanizeText(taskUpdate.taskState)}
        />
        <Column
          label="Message"
          id="message"
          key="message"
          cellData={(taskUpdate) => taskUpdate.statusMessage && taskUpdate.statusMessage || '—'}
        />
        <Column
          label="Time"
          id="time"
          key="time"
          cellData={(taskUpdate) => Utils.timestampFromNow(taskUpdate.timestamp)}
        />
      </UITable>
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
