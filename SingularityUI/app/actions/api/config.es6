import { buildJsonApiAction } from './base';

export const EnableRackSensitivity = buildJsonApiAction(
  'ENABLE_RACK_SENSITIVITY',
  'POST',
  {url: '/configuration/rack-sensitive/enable'}
);

export const DisableRackSensitivity = buildJsonApiAction(
  'DISABLE_RACK_SENSITIVITY',
  'POST',
  {url: '/configuration/rack-sensitive/disable'}
);


export const OverridePlacementStrategy = buildJsonApiAction(
  'SET_PLACEMENT_STRATEGY',
  'POST',
  strategy => {
    if (!strategy) {
      strategy = '';
    }

    return {
      url: `/configuration/placement-strategy/override/set/${strategy}`
    };
  }
);
