/*
   Copyright 2013 Rene Nowak 

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package at.ac.tuwien.infosys.jcloudscale.datastore.mapping.type;

public final class TypeToken<T, K> {

    private Class<? super T> rawType;
    private TypeAdapter<T, K> typeAdapter;

    //Hide private constructor
    private TypeToken() {}

    public TypeToken(Class<? super T> rawType, TypeAdapter<T, K> typeAdapter) {
        this.rawType = rawType;
        this.typeAdapter = typeAdapter;
    }

    public boolean isAssignableFrom(Class<?> clazz) {
        return rawType.isAssignableFrom(clazz);
    }

    public TypeAdapter<T, K> getTypeAdapter() {
        return typeAdapter;
    }
}
