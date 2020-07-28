/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.init.func;

import java.util.Set;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author Dave Syer
 *
 */
public interface ImportRegistrars {

	void add(Class<?> importer, Class<?> imported);

	Set<Imported> getImports();

	void defer(ApplicationContextInitializer<?>... initializers);
	
	void processDeferred(GenericApplicationContext context);

	public static class Imported {

		private Class<?> source;

		private Class<?> type;

		private String resources;

		public Imported(Class<?> source, Class<?> type) {
			this.source = source;
			this.type = type;
		}

		public Imported(Class<?> source, String resources) {
			this.source = source;
			this.resources = resources;
		}

		public String getResources() {
			return resources;
		}

		public Class<?> getSource() {
			return this.source;
		}

		public Class<?> getType() {
			return this.type;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Imported other = (Imported) obj;
			if (resources == null) {
				if (other.resources != null)
					return false;
			} else if (!resources.equals(other.resources))
				return false;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((resources == null) ? 0 : resources.hashCode());
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return "Imported [source=" + this.source.getName()

					+ ", location=" + (this.type == null ? this.resources == null ? "" : resources : type.getName())
					+ "]";
		}

	}

}
