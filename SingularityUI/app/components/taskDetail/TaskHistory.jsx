import React, { PropTypes } from 'react';
import Utils from '../../utils';
import Section from '../common/Section';
import UITable from '../common/table/UITable';
import Column from '../common/table/Column';
import classNames from 'classnames';

function TaskHistory (props) {
  return (
    <Section title="History">
      <UITable
        emptyTableMessage="This task has no history yet"
        data={props.taskUpdates.concat().reverse()}
        keyGetter={(taskUpdate) => taskUpdate.timestamp}
        rowChunkSize={5}
        paginated={true}
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
