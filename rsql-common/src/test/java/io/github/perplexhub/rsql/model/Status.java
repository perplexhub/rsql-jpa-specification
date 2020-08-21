package io.github.perplexhub.rsql.model;

import javax.persistence.AttributeConverter;

public enum Status {

	STARTED, ACTIVE, CANCELLED, FINISHED;

	public static class Converter implements AttributeConverter<Status, String> {

		@Override
		public String convertToDatabaseColumn(Status attribute) {
			return (attribute == null) ? null : attribute.name();
		}

		@Override
		public Status convertToEntityAttribute(String dbData) {
			return dbData == null ? null : Status.valueOf(dbData);
		}

	}
}
