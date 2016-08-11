export const ModifyField = (formId, fieldId, newValue) => ({
  formId,
  fieldId,
  newValue,
  type: 'MODIFY_FORM_FIELD'
});

export const ClearField = (formId, fieldId) => ({
  formId,
  fieldId,
  type: 'CLEAR_FORM_FIELD'
});

export const ClearForm = (formId) => ({
  formId,
  type: 'CLEAR_FORM'
});
