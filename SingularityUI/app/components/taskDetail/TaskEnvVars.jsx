import React from 'react';
import Utils from '../../utils';
import { InfoBox, UsageInfo } from '../common/statelessComponents';
import { Alert } from 'react-bootstrap';
import FormGroup from 'react-bootstrap/lib/FormGroup';

import JSONButton from '../common/JSONButton';
import Section from '../common/Section';
import ConfirmationDialog from '../common/ConfirmationDialog';
import CollapsableSection from '../common/CollapsableSection';
import SimpleTable from '../common/SimpleTable';
import Glyphicon from '../common/atomicDisplayItems/Glyphicon';

export default (props) => {
  const t = props.task;
  if (!t.task.mesosTask.executor) return null;
  let vars = [];
  for (let v of t.task.mesosTask.executor.command.environment.variables) {
    vars.push(<InfoBox key={v.name} copyableClassName="info-copyable" name={v.name} value={v.value} />);
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
