package com.example.adoptme.data

import com.example.adoptme.R
import com.example.adoptme.model.Pet

class PetRepository {

    fun getPets(): List<Pet> {

        return listOf(

            Pet(
                id = 1,
                name = "Max",
                species = "Perro",
                breed = "Labrador",
                age = "2 años",
                gender = "Macho",
                location = "Miraflores",
                status = "Disponible",
                description = "Max es un perro amigable, cariñoso y juguetón.",
                image = R.drawable.max
            ),

            Pet(
                id = 2,
                name = "Luna",
                species = "Gato",
                breed = "Gato doméstico",
                age = "1 año",
                gender = "Hembra",
                location = "San Isidro",
                status = "Disponible",
                description = "Luna es una gatita tranquila y cariñosa.",
                image = R.drawable.luna
            ),

            Pet(
                id = 3,
                name = "Rocky",
                species = "Perro",
                breed = "Golden Retriever",
                age = "3 años",
                gender = "Macho",
                location = "Surco",
                status = "En proceso",
                description = "Rocky es activo, juguetón y muy sociable.",
                image = R.drawable.rocky
            ),

            Pet(
                id = 4,
                name = "Mia",
                species = "Gato",
                breed = "Angora",
                age = "2 años",
                gender = "Hembra",
                location = "Barranco",
                status = "Adoptada",
                description = "Mia es una gata tranquila que disfruta de la compañía.",
                image = R.drawable.mia
            ),

            Pet(
                id = 5,
                name = "Tambor",
                species = "Conejo",
                breed = "Cabeza de León",
                age = "8 meses",
                gender = "Macho",
                location = "San Borja",
                status = "Disponible",
                description = "Tambor es un conejito curioso, dócil y muy sociable.",
                image = R.drawable.luna
            ),

            Pet(
                id = 6,
                name = "Copito",
                species = "Conejo",
                breed = "Enano Holandés",
                age = "1 año",
                gender = "Hembra",
                location = "La Molina",
                status = "Disponible",
                description = "Copito es tranquila, limpia y acostumbrada a convivir en casa.",
                image = R.drawable.mia
            ),

            Pet(
                id = 7,
                name = "Bobby",
                species = "Perro",
                breed = "Labrador",
                age = "2 años",
                gender = "Macho",
                location = "Miraflores, Lima",
                status = "Disponible",
                description = "Bobby es un perro labrador noble, muy juguetón y lleno de energía.",
                image = R.drawable.bobby,
                creadoPorCorreo = ""
            ),

            Pet(
                id = 8,
                name = "Toby",
                species = "Perro",
                breed = "Mestizo",
                age = "1 año",
                gender = "Macho",
                location = "Surco, Lima",
                status = "Disponible",
                description = "Toby es un perrito joven, cariñoso y muy sociable con otros animales.",
                image = R.drawable.toby,
                creadoPorCorreo = ""
            ),

            Pet(
                id = 9,
                name = "Rocky",
                species = "Perro",
                breed = "Pastor",
                age = "3 años",
                gender = "Macho",
                location = "San Borja, Lima",
                status = "Disponible",
                description = "Rocky es un pastor leal, inteligente y protector, ideal para familias activas.",
                image = R.drawable.rocky_nuevo,
                creadoPorCorreo = ""
            ),

            Pet(
                id = 10,
                name = "Nala",
                species = "Gato",
                breed = "Doméstico de pelo corto",
                age = "2 años",
                gender = "Hembra",
                location = "San Isidro, Lima",
                status = "Disponible",
                description = "Nala es una gatita dulce, mimosa y tranquila, le fascina descansar al sol.",
                image = R.drawable.nala,
                creadoPorCorreo = ""
            ),

            Pet(
                id = 11,
                name = "Michi",
                species = "Gato",
                breed = "Atigrado",
                age = "1 año",
                gender = "Macho",
                location = "La Molina, Lima",
                status = "Disponible",
                description = "Michi es muy activo y curioso, adora perseguir juguetes y recibir cariño.",
                image = R.drawable.michi,
                creadoPorCorreo = ""
            ),

            Pet(
                id = 12,
                name = "Simba",
                species = "Gato",
                breed = "Mestizo",
                age = "3 años",
                gender = "Macho",
                location = "Callao, Lima",
                status = "Disponible",
                description = "Simba es un gato sereno, independiente y excelente compañero en casa.",
                image = R.drawable.simba,
                creadoPorCorreo = ""
            ),

            Pet(
                id = 13,
                name = "Bunny",
                species = "Conejo",
                breed = "Holland Lop",
                age = "8 meses",
                gender = "Macho",
                location = "Surquillo, Lima",
                status = "Disponible",
                description = "Bunny es un conejito dócil y curioso de orejitas caídas, ideal para un hogar calmado.",
                image = R.drawable.bunny,
                creadoPorCorreo = ""
            ),

            Pet(
                id = 14,
                name = "Moka",
                species = "Conejo",
                breed = "Cabeza de León",
                age = "1 año",
                gender = "Hembra",
                location = "Ate, Lima",
                status = "Disponible",
                description = "Moka es una conejita esponjosa y muy tierna, acostumbrada a los cuidados del hogar.",
                image = R.drawable.moka,
                creadoPorCorreo = ""
            ),

            Pet(
                id = 15,
                name = "Nube",
                species = "Conejo",
                breed = "Mini Rex",
                age = "2 años",
                gender = "Hembra",
                location = "Comas, Lima",
                status = "Disponible",
                description = "Nube es suave, limpia y de carácter pacífico, muy cariñosa y tranquila.",
                image = R.drawable.nube,
                creadoPorCorreo = ""
            )
        )
    }
}