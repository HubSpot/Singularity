import React, { PropTypes } from 'react';
import { InfoBox } from '../common/statelessComponents';

import CollapsableSection from '../common/CollapsableSection';

function TaskEnvVars (props) {
  if (!props.task.task.mesosTask.executor) return null;
  let vars = [];
  for (const variable of props.task.task.mesosTask.executor.command.environment.variables) {
    vars.push(<InfoBox key={variable.name} copyableClassName="info-copyable" name={variable.name} value={variable.value} />);
  }

  return (
    <CollapsableSection title="Environment variables">
      <div className="row">
        <ul className="list-unstyled horizontal-description-list">
          {vars}
        </ul>
      </div>
    </CollapsableSection>
  );
}

TaskEnvVars.propTypes = {
  task: PropTypes.shape({
    task: PropTypes.shape({
      mesosTask: PropTypes.shape({
        executor: PropTypes.shape({
          command: PropTypes.shape({
            environment: PropTypes.shape({
              variables: PropTypes.arrayOf(PropTypes.shape({
                name: PropTypes.string,
                value: PropTypes.string
              }))
            }).isRequired
          }).isRequired
        })
      }).isRequired
    }).isRequired
  }).isRequired
};

export default TaskEnvVars;
