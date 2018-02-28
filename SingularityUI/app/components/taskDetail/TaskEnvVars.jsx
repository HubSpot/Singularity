import React, { PropTypes } from 'react';
import { InfoBox } from '../common/statelessComponents';

import CollapsableSection from '../common/CollapsableSection';

function TaskEnvVars (props) {
  if (!props.executor) return null;
  let vars = [];
  for (const variable of _.sortBy(props.executor.command.environment.variables, 'name')) {
    vars.push(<InfoBox key={variable.name} name={variable.name} value={variable.value} />);
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
};

export default TaskEnvVars;
