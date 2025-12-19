import {inject, Injectable} from '@angular/core';
import {Api} from '../api/generated/api';
import {getTemplates} from '../api/generated/fn/templates/get-templates';
import {updateTemplate} from '../api/generated/fn/templates/update-template';
import {listOverrides} from '../api/generated/fn/templates/list-overrides';
import {setOverride} from '../api/generated/fn/templates/set-override';
import {deleteOverride} from '../api/generated/fn/templates/delete-override';
import {copyOverrides} from '../api/generated/fn/templates/copy-overrides';
import {
  BooleanTemplateField,
  CopyOverridesResponse,
  EnumTemplateField,
  FieldType,
  NumberTemplateField,
  StringTemplateField,
  TemplateField,
  TemplateResponse,
  TemplateSchema,
  TemplateType,
  TemplateValuesResponse
} from '../api/generated/models';

@Injectable({providedIn: 'root'})
export class TemplateService {
  private api = inject(Api);

  async getTemplates(applicationId: string, type?: TemplateType): Promise<TemplateResponse[]> {
    return this.api.invoke(getTemplates, {applicationId, type});
  }

  async getTemplate(applicationId: string, type: TemplateType): Promise<TemplateResponse | undefined> {
    const templates = await this.getTemplates(applicationId, type);
    return templates.find(t => t.type === type);
  }

  async updateTemplate(applicationId: string, type: TemplateType, schema: TemplateSchema): Promise<TemplateResponse> {
    return this.api.invoke(updateTemplate, {
      applicationId,
      type,
      body: {schema}
    });
  }

  async getOverrides(applicationId: string, type: TemplateType, environmentId: string): Promise<TemplateValuesResponse[]> {
    return this.api.invoke(listOverrides, {applicationId, type, environmentId});
  }

  async setOverride(
    applicationId: string,
    type: TemplateType,
    environmentId: string,
    identifier: string,
    values: Record<string, unknown>
  ): Promise<TemplateValuesResponse> {
    return this.api.invoke(setOverride, {
      applicationId,
      type,
      environmentId,
      identifier,
      body: {values: values as { [key: string]: {} }}
    });
  }

  async deleteOverride(
    applicationId: string,
    type: TemplateType,
    environmentId: string,
    identifier: string
  ): Promise<void> {
    await this.api.invoke(deleteOverride, {applicationId, type, environmentId, identifier});
  }

  async copyOverrides(
    applicationId: string,
    sourceEnvironmentId: string,
    targetEnvironmentId: string,
    types?: TemplateType[],
    overwrite?: boolean,
    identifiers?: string[]
  ): Promise<CopyOverridesResponse> {
    return this.api.invoke(copyOverrides, {
      applicationId,
      body: {
        sourceEnvironmentId,
        targetEnvironmentId,
        types,
        overwrite,
        identifiers
      }
    });
  }

  isStringField(field: TemplateField): field is StringTemplateField {
    return field.type === FieldType.String;
  }

  isNumberField(field: TemplateField): field is NumberTemplateField {
    return field.type === FieldType.Number;
  }

  isBooleanField(field: TemplateField): field is BooleanTemplateField {
    return field.type === FieldType.Boolean;
  }

  isEnumField(field: TemplateField): field is EnumTemplateField {
    return field.type === FieldType.Enum;
  }

  getDefaultValue(field: TemplateField): unknown {
    switch (field.type) {
      case FieldType.String:
        return (field as StringTemplateField).defaultValue;
      case FieldType.Number:
        return (field as NumberTemplateField).defaultValue;
      case FieldType.Boolean:
        return (field as BooleanTemplateField).defaultValue;
      case FieldType.Enum:
        return (field as EnumTemplateField).defaultValue;
      default:
        return undefined;
    }
  }

  formatConstraints(field: TemplateField): string {
    switch (field.type) {
      case FieldType.String: {
        const stringField = field as StringTemplateField;
        const parts: string[] = [];
        if (stringField.minLength !== undefined && stringField.minLength > 0) {
          parts.push(`min ${stringField.minLength}`);
        }
        if (stringField.maxLength !== undefined) {
          parts.push(`max ${stringField.maxLength} chars`);
        }
        return parts.join(', ');
      }
      case FieldType.Number: {
        const numberField = field as NumberTemplateField;
        const {minValue, maxValue, incrementAmount} = numberField;
        const parts: string[] = [];
        if (minValue !== undefined && maxValue !== undefined) {
          parts.push(`${minValue}–${maxValue}`);
        } else if (minValue !== undefined) {
          parts.push(`≥${minValue}`);
        } else if (maxValue !== undefined) {
          parts.push(`≤${maxValue}`);
        }
        if (incrementAmount !== undefined && incrementAmount !== 1) {
          parts.push(`step ${incrementAmount}`);
        }
        return parts.join(', ');
      }
      case FieldType.Enum: {
        const enumField = field as EnumTemplateField;
        return enumField.options?.join(', ') ?? '';
      }
      default:
        return '';
    }
  }

  createStringField(key: string, description: string, editable: boolean, defaultValue: string, minLength: number, maxLength: number): StringTemplateField {
    return {
      key,
      type: FieldType.String,
      description,
      editable,
      defaultValue,
      minLength,
      maxLength
    };
  }

  createNumberField(key: string, description: string, editable: boolean, defaultValue: number, minValue: number, maxValue: number, incrementAmount: number): NumberTemplateField {
    return {
      key,
      type: FieldType.Number,
      description,
      editable,
      defaultValue,
      minValue,
      maxValue,
      incrementAmount
    };
  }

  createBooleanField(key: string, description: string, editable: boolean, defaultValue: boolean): BooleanTemplateField {
    return {
      key,
      type: FieldType.Boolean,
      description,
      editable,
      defaultValue
    };
  }

  createEnumField(key: string, description: string, editable: boolean, defaultValue: string, options: string[]): EnumTemplateField {
    return {
      key,
      type: FieldType.Enum,
      description,
      editable,
      defaultValue,
      options
    };
  }
}
