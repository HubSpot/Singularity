export const modifyField = (formId, fieldId, newValue) =>
    ({
        formId,
        fieldId,
        newValue,
        type: 'MODIFY_FORM_FIELD'
    })
;

export const clearField = (formId, fieldId) =>
    ({
        formId,
        fieldId,
        type: 'CLEAR_FORM_FIELD'
    })
;

export const clearForm = (formId) =>
    ({
        formId,
        type: 'CLEAR_FORM'
    })
;

export default {modifyField, clearField, clearForm};
